"use client"
import React, {useEffect, useState} from 'react';
import {createTheme, ThemeProvider} from "@mui/material/styles";
import {
    Alert,
    Box,
    Button,
    Card,
    CardActions,
    CardContent,
    Container,
    CssBaseline,
    Dialog,
    DialogActions,
    DialogContent,
    DialogContentText,
    DialogTitle,
    IconButton,
    InputAdornment,
    Link,
    Snackbar,
    Stack,
    TextField,
    Typography,
} from "@mui/material";
import {Add as AddIcon, Delete as DeleteIcon, Edit as EditIcon, Search as SearchIcon,} from '@mui/icons-material';
import useMediaQuery from "@mui/material/useMediaQuery";
import {BACKEND_SERVER_URL_BOOKMARK} from "@/app/AppEnv";
import {Bookmark} from "@/app/models";

interface BookmarkFormData {
    name: string;
    url: string;
    description: string;
}

const initialFormData: BookmarkFormData = {
    name: '',
    url: '',
    description: '',
};

export default function BookmarkPage() {
    const [bookmarks, setBookmarks] = useState<Bookmark[]>([]); // https://github.com/tedwon/platypus-upstream/issues/8
    const [openDialog, setOpenDialog] = useState(false);
    const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
    const [bookmarkToDelete, setBookmarkToDelete] = useState<number | null>(null);
    const [formData, setFormData] = useState<BookmarkFormData>(initialFormData);
    const [editingId, setEditingId] = useState<number | null>(null);
    const [searchQuery, setSearchQuery] = useState('');
    const [snackbar, setSnackbar] = useState({
        open: false,
        message: '',
        severity: 'success' as 'success' | 'error'
    });

    const prefersDarkMode = useMediaQuery('(prefers-color-scheme: dark)');
    const theme = React.useMemo(
        () => createTheme({
            palette: {
                mode: prefersDarkMode ? 'dark' : 'light',
            },
        }),
        [prefersDarkMode],
    );

    const fetchBookmarks = async () => {
        try {
            const response = await fetch(BACKEND_SERVER_URL_BOOKMARK);
            const data = await response.json();
            setBookmarks(data);
        } catch (error) {
            console.error('Error fetching bookmarks:', error);
            showSnackbar('Failed to load bookmarks', 'error');
        }
    };

    useEffect(() => {
        fetchBookmarks();
    }, []);

    const handleSearch = async () => {
        if (!searchQuery.trim()) {
            fetchBookmarks();
            return;
        }
        try {
            const response = await fetch(`${BACKEND_SERVER_URL_BOOKMARK}/search?q=${encodeURIComponent(searchQuery)}`);
            const data = await response.json();
            setBookmarks(data);
        } catch (error) {
            console.error('Error searching bookmarks:', error);
            showSnackbar('Search failed', 'error');
        }
    };

    const showSnackbar = (message: string, severity: 'success' | 'error') => {
        setSnackbar({open: true, message, severity});
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            const url = editingId
                ? `${BACKEND_SERVER_URL_BOOKMARK}/${editingId}`
                : BACKEND_SERVER_URL_BOOKMARK;

            const response = await fetch(url, {
                method: editingId ? 'PUT' : 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(formData),
            });

            if (response.ok) {
                showSnackbar(
                    editingId ? 'Bookmark updated successfully' : 'Bookmark created successfully',
                    'success'
                );
                handleCloseDialog();
                fetchBookmarks();
            } else {
                throw new Error('Operation failed');
            }
        } catch (error) {
            console.error('Error saving bookmark:', error);
            showSnackbar(
                editingId ? 'Failed to update bookmark' : 'Failed to create bookmark',
                'error'
            );
        }
    };

    const handleDeleteClick = (id: number) => {
        setBookmarkToDelete(id);
        setDeleteDialogOpen(true);
    };

    const handleDeleteConfirm = async () => {
        if (bookmarkToDelete === null) return;

        try {
            const response = await fetch(`${BACKEND_SERVER_URL_BOOKMARK}/${bookmarkToDelete}`, {
                method: 'DELETE',
            });

            if (response.ok) {
                showSnackbar('Bookmark deleted successfully', 'success');
                fetchBookmarks();
            } else {
                throw new Error('Delete failed');
            }
        } catch (error) {
            console.error('Error deleting bookmark:', error);
            showSnackbar('Failed to delete bookmark', 'error');
        } finally {
            setDeleteDialogOpen(false);
            setBookmarkToDelete(null);
        }
    };

    const handleDeleteCancel = () => {
        setDeleteDialogOpen(false);
        setBookmarkToDelete(null);
    };

    const handleEdit = (bookmark: Bookmark) => {
        setEditingId(bookmark.id);
        setFormData({
            name: bookmark.name,
            url: bookmark.url,
            description: bookmark.description,
        });
        setOpenDialog(true);
    };

    const handleCloseDialog = () => {
        setOpenDialog(false);
        setEditingId(null);
        setFormData(initialFormData);
    };

    return (
        <ThemeProvider theme={theme}>
            <CssBaseline/>
            <Container maxWidth="xl">
                <Box sx={{width: '100%', py: 4}}>
                    <Stack spacing={3}>
                        <Box sx={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
                            <Typography variant="h4" component="h1">
                                Bookmarks
                            </Typography>
                            <Button
                                variant="contained"
                                startIcon={<AddIcon/>}
                                onClick={() => setOpenDialog(true)}
                            >
                                Add Bookmark
                            </Button>
                        </Box>

                        <TextField
                            fullWidth
                            variant="outlined"
                            placeholder="Search bookmarks..."
                            value={searchQuery}
                            onChange={(e) => setSearchQuery(e.target.value)}
                            onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
                            InputProps={{
                                startAdornment: (
                                    <InputAdornment position="start">
                                        <SearchIcon/>
                                    </InputAdornment>
                                ),
                            }}
                        />

                        <Stack spacing={2}>
                            {bookmarks.map((bookmark) => (
                                <Card key={bookmark.id}>
                                    <CardContent>
                                        <Typography variant="h6" component="h2">
                                            <Link
                                                href={bookmark.url}
                                                underline="hover"
                                                sx={{
                                                    color: 'primary.main',
                                                    fontWeight: 'bold',
                                                    fontSize: '18px',
                                                }}
                                                target="_blank"
                                            >
                                                {bookmark.name}
                                            </Link>
                                        </Typography>
                                        <Typography variant="body1">
                                            {bookmark.description}
                                        </Typography>
                                    </CardContent>
                                    <CardActions>
                                        <IconButton onClick={() => handleEdit(bookmark)}>
                                            <EditIcon/>
                                        </IconButton>
                                        <IconButton onClick={() => handleDeleteClick(bookmark.id)}>
                                            <DeleteIcon/>
                                        </IconButton>
                                    </CardActions>
                                </Card>
                            ))}
                        </Stack>
                    </Stack>
                </Box>

                <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
                    <DialogTitle>
                        {editingId ? 'Edit Bookmark' : 'Add New Bookmark'}
                    </DialogTitle>
                    <form onSubmit={handleSubmit}>
                        <DialogContent>
                            <Stack spacing={2}>
                                <TextField
                                    fullWidth
                                    label="Name"
                                    value={formData.name}
                                    onChange={(e) => setFormData({...formData, name: e.target.value})}
                                    required
                                />
                                <TextField
                                    fullWidth
                                    label="URL"
                                    value={formData.url}
                                    onChange={(e) => setFormData({...formData, url: e.target.value})}
                                    required
                                />
                                <TextField
                                    fullWidth
                                    label="Description"
                                    value={formData.description}
                                    onChange={(e) => setFormData({...formData, description: e.target.value})}
                                    multiline
                                    rows={4}
                                />
                            </Stack>
                        </DialogContent>
                        <DialogActions>
                            <Button onClick={handleCloseDialog}>Cancel</Button>
                            <Button type="submit" variant="contained">
                                {editingId ? 'Update' : 'Create'}
                            </Button>
                        </DialogActions>
                    </form>
                </Dialog>

                {/* Delete Confirmation Dialog */}
                <Dialog
                    open={deleteDialogOpen}
                    onClose={handleDeleteCancel}
                >
                    <DialogTitle>Delete Bookmark</DialogTitle>
                    <DialogContent>
                        <DialogContentText>
                            Are you sure you want to delete this bookmark? This action cannot be undone.
                        </DialogContentText>
                    </DialogContent>
                    <DialogActions>
                        <Button onClick={handleDeleteCancel}>Cancel</Button>
                        <Button onClick={handleDeleteConfirm} color="error" variant="contained">
                            Delete
                        </Button>
                    </DialogActions>
                </Dialog>

                <Snackbar
                    open={snackbar.open}
                    autoHideDuration={6000}
                    onClose={() => setSnackbar({...snackbar, open: false})}
                >
                    <Alert severity={snackbar.severity} onClose={() => setSnackbar({...snackbar, open: false})}>
                        {snackbar.message}
                    </Alert>
                </Snackbar>
            </Container>
        </ThemeProvider>
    );
}